package com.example.demo.controllers;

import com.example.demo.AttendanceWrapper;
import com.example.demo.Utilities;
import com.example.demo.models.Attendance;
import com.example.demo.models.Course;
import com.example.demo.models.Person;
import com.example.demo.models.RegistrationTimestamp;
import com.example.demo.repositories.*;
import com.example.demo.services.UserService;
import com.google.common.collect.Lists;
import it.ozimov.springboot.mail.model.Email;
import it.ozimov.springboot.mail.model.defaultimpl.DefaultEmail;
import it.ozimov.springboot.mail.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.mail.internet.InternetAddress;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class TeacherController {
	@Autowired
	AuthorityRepo authorityRepo;
	@Autowired
	PersonRepo personRepo;
	@Autowired
	CourseRepo courseRepo;
	@Autowired
	AttendanceRepo attendanceRepo;
	@Autowired
	RegistrationTimestampRepo registrationTimestampRepo;
	@Autowired
	EvaluationRepo evaluationRepo;

	@Autowired
	private UserService userService;

	@Autowired
	public EmailService emailService;

	//List of courses for a particular Teacher
	@RequestMapping("/mycoursesdetail")
	public String listTeacherCourses(Principal principal, Model model) {
		Person teacher = personRepo.findByUsername(principal.getName());
		model.addAttribute("teachercourse", teacher);
		model.addAttribute("courselist", courseRepo.findByPersonsIsAndDeletedIs(teacher, false));
		return "teachercoursedetail";
	}


	//List of Students for a particular Course
	// path variable is the course id
	@RequestMapping("/viewregisteredstudent/{id}")
	public String listRegisteredStud(@PathVariable("id") long id, Model model, Principal principal) {

		model.addAttribute("liststudent",
				personRepo.findByCoursesIsAndAuthoritiesIsOrderByNameLastAsc(courseRepo.findOne(id),
						authorityRepo.findByRole("STUDENT")));

		model.addAttribute("courseId", id);
		return "listregisteredstudent";
	}


	//List of Student attendance for a particular course
	// TODO I DO NOT THINK THIS ROUTE EVEN NEEDS TO EXIST!!!
	// requirements state that teacher should be able to EMAIL attendance details to the admin
	// there is actually nothing in requirements that state that a teacher or admin should be able to
	// VIEW the attendance in any way, other than via email
//	@RequestMapping("/viewattendance/{id}")
//	public String listStudAttendance(@PathVariable("id") long courseId, Model model, Principal principal) {
//		model.addAttribute("listattendance", attendanceRepo.findByPersonIsAndCourseIsOrderByDateAsc(personRepo.
//				findByUsername(principal.getName()), courseRepo.findOne(courseId)));
//
//		return "viewstudentattendance";
//	}


	//Display course evealuation
	// TODO WAITING FOR JESSE
	@RequestMapping("/dispevaluation/{id}")
	public String dipCourseEvaluation(@PathVariable("id") long id, Model model) {
		model.addAttribute("dispEval", evaluationRepo.findAll());
		return "dispevaluation";
	}


	@RequestMapping("/addstudent/{id}")
	public String registerStudent(@PathVariable("id") long id, Model model) {
		model.addAttribute("newstudent", new Person());
		Course course = courseRepo.findOne(id);
		model.addAttribute("course", course);
		return "addstudenttocourse";
	}


	// students do not have usernames or passwords, but they must enter first, last names
	// and contact num, email
	@PostMapping("/addstudent/{id}")
	public String addStudentToCourse(@PathVariable("id") long courseId,
									 @Valid @ModelAttribute("newstudent") Person student,
									 BindingResult bindingResult, Model model) {

		if(bindingResult.hasErrors()) {
			Course course = courseRepo.findOne(courseId);
			model.addAttribute("course", course);
			return "addstudenttocourse";
		}


		RegistrationTimestamp timestamp = new RegistrationTimestamp();
		Person p=userService.saveStudent(student);
		Course course = courseRepo.findOne(courseId);
		timestamp.setCourse(course);
		timestamp.setPerson(p);
		timestamp.setTimestamp(new Date());
		registrationTimestampRepo.save(timestamp);
		course.addPerson(p);
		courseRepo.save(course);

		// TODO: if we have time, it would be nice to have some sort of confirmation that a student was registered to the course
		return "redirect:/addstudent/" + courseId;
	}



	// UNDER CONSTRUCTION BUT IS SAVING A LIST OF ATTENDANCE OBJECTS TO DB!
	// NOTE: for now, we are taking attendance for one student at a time, not ideal, but works..
	// will update to do all students in course on one page if there is enough time
	@GetMapping("/takeattendance/{courseid}")
	public String takeAttendance(@PathVariable("courseid") long courseId,
								 @RequestParam("studentid") long studentid, Model model) {

		// get the course we are taking attendance for
		Course course = courseRepo.findOne(courseId);
		// get the student we are taking attendance for
		Person student = personRepo.findOne(studentid);
		System.out.println("=================== fist name of student who we are taking attendance for (person.getNameFirst): " + student.getNameFirst());
		System.out.println("=================== id of student who we are taking attendance for (person.getId): " + student.getId());

		// get the difference in days between course start and end dates
		int diffInDays = Utilities.getDiffInDays(course.getDateStart(), course.getDateEnd());
		System.out.printf("======================= Difference between course start and end dates: %d day(s)", diffInDays);

		// need this to be able to process a list of objects in a single form
		AttendanceWrapper wrapper = new AttendanceWrapper();
		// get the course start date
		Date startDate = course.getDateStart();
		// create an empty list of attendance
		List<Attendance> attendanceArrayList = new ArrayList<>();

		// now create diffInDays number of Attendance objects to send to view
		for (int i = 0; i < diffInDays; i++) {
			Attendance attendance = new Attendance();
			// set the person
			attendance.setPerson(student);
			// set the course
			attendance.setCourse(course);
			// set the date, increment by one day for each new Attendance object
			attendance.setDate(Utilities.addDays(startDate, i));
			// add it to the list
			attendanceArrayList.add(attendance);

		}
		wrapper.setAttendanceList(attendanceArrayList);

		model.addAttribute("attendanceWrapper", wrapper);
		model.addAttribute("studentName", student.getNameFirst() + ' ' + student.getNameLast());
		model.addAttribute("courseName", course.getName());
		model.addAttribute("courseId", courseId);

		return "takeattendance";
	}


	@PostMapping("/takeattendance/{courseid}")
	public String takeAttendancePost(
			@ModelAttribute("attendanceWrapper") AttendanceWrapper attWrapper,
			@PathVariable("courseid") long courseId) {

		System.out.println("================================================ in /takeattendance POST, incoming courseId: " + courseId);
		System.out.println("=================== attWrapper.getStringList.size: " + attWrapper.getAttendanceList().size());

		// courseId, personId, and date are all preserved through the form, so just need to save it now, both join columns are set
		attendanceRepo.save(attWrapper.getAttendanceList());

		return "redirect:/viewregisteredstudent/" + courseId;
	}


	// there is no requirement to notify anyone when a course is deleted so I don't think we need this
	// we can do something here after we get the requirements done
//	@RequestMapping("/endcourse/{courseid}")
//	public String endClass() {
//		System.out.println("Send email to admin");
//		return "endcourse";
//	}


	// shows a drop down list of admins, teacher selects one to send an attendance email to
	@GetMapping("/sendemail")
	public String sendEmail (@RequestParam("id") long courseId, Model model) {



		// first add a list of admins to the template
		model.addAttribute("adminList", personRepo.findByAuthoritiesIs(authorityRepo.findByRole("ADMIN")));
		// add the course to the model, so we can show the name on the page
		model.addAttribute("course", courseRepo.findOne(courseId));

		return "sendemail";
	}


	@PostMapping("/sendemail")
	public String sendEmailPost(@RequestParam("selectedAdminId") long adminId,
								@RequestParam("courseId") long courseId,
								Principal principal) {

		System.out.println("=================== in /sendemail POST, selectedAdminId: " + adminId);

		// get the logged in person
		Person teacher = personRepo.findByUsername(principal.getName());

		// get the selected admin
		Person admin = personRepo.findOne(adminId);

		// get the course
		Course course = courseRepo.findOne(courseId);

		// build the email body
		String body = buildAttendanceEmail(course);

		// testing
		System.out.println(body);


		// the email needs to know what admin addres to send to
		// sendemail.html has a drop down list of admins, teacher selects
		// one to send the attendance info to
		// TODO it would be nice if we could force this to be a fixed width font, because it looks poor with non fixed width
		sendEmailWithoutTemplate(
				teacher.getFullName(),	// teacher name
				course.getName(),		// course name
				body,					// email body
				admin.getEmail(),		// to email address
				admin.getFullName());	// to email name

		return "redirect:/mycoursesdetail";
	}


	// builds a String that has all the attendance info for a single course
	// result is a basic text based table, will only look nice with a fixed width font
	private String buildAttendanceEmail(Course course) {
		int diffInDays = Utilities.getDiffInDays(course.getDateStart(), course.getDateEnd());

		Set<Person> students = personRepo.findByCoursesIsAndAuthoritiesIsOrderByNameLastAsc(course, authorityRepo.findByRole("STUDENT"));

		SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");


		String s = String.format("%-16s %-10s", "LAST NAME", "mNUM");

		System.out.println("!!!!!!!!!!!!!!!!!!!!!!! inside buildEmail.. diffInDays: " + diffInDays);

		// create a header row
		for (int i = 0; i < diffInDays; i++) {
			s += String.format("%-10s", dateFormat.format(Utilities.addDays(course.getDateStart(), i)));
		}

		// add a horizontal line
		s += "\n-------------------------"; // 26
		for(int i = 0; i < diffInDays; i++) {
			s += "----------"; // 10
		}

		s += "\n";

		for (Person p : students) {
			s += String.format("%-16s %-10s", p.getNameLast(), p.getmNumber());
			for (Attendance a : attendanceRepo.findByPersonIsAndCourseIsOrderByDateAsc(p, course)) {
				s += String.format("%-10s", a.getAstatus());
			}
			s += "\n";
		}

		return s;
	}


	//Email Sending to admin from the teacher

	public void sendEmailWithoutTemplate(String teacherName, String courseName,String eBody,String adminEmail,String adminName) {

		final Email email;
		try {
			email = DefaultEmail.builder()
					// DOES NOT MATTER what you put in .from address.. it ignores it and uses what is in properties file
					// this may work depending on the email server config that is being used
					// the from NAME does get used though
					.from(new InternetAddress("anyone@anywhere.net", teacherName))
					.to(Lists.newArrayList(
							new InternetAddress(adminEmail, adminName)))
					.subject("Attendance For "+ courseName)
					.body(eBody)
					.encoding("UTF-8").build();

			// conveniently, .send will put a nice INFO message in the console output when it sends
			emailService.send(email);

		} catch (UnsupportedEncodingException e) {
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!! caught an unsupported encoding exception");
			e.printStackTrace();
		}



	}


}
