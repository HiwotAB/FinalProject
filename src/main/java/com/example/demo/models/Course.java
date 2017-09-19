package com.example.demo.models;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

@Entity
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    // @Temporal required for validation
    @NotNull
    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "MMM d, yyyy")
    private Date dateStart;

    @NotNull
    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "MMM d, yyyy")
    private Date dateEnd;

    @NotEmpty
    private String name;

    @Min(6)
    private long courseRegistrationNum;

    // 0 = false, 1 = true
    private boolean deleted;

    private long numEvaluationsCompleted;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private Collection<RegistrationTimestamp> timeStamps;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private Collection<Evaluation> evaluations;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private Collection<Attendance> attendances;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private Collection<CourseInfoRequestLog> courseInfoRequestLogs;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns = @JoinColumn(name = "course_id"), inverseJoinColumns = @JoinColumn(name = "person_id"))
    private Collection<Person> persons;


    public Course() {
        this.timeStamps = new HashSet<>();
        this.evaluations = new HashSet<>();
        this.attendances = new HashSet<>();
        this.persons = new HashSet<>();
        this.courseInfoRequestLogs = new HashSet<>();
    }

    // helper methods ==================================================================================
    public void addPerson(Person person) {
        persons.add(person);
    }

    // use this to display the deleted status in the evaluations table
    public String getDeletedString() {
        return deleted ? "YES" : "NO";
    }

    public long getNumStudents(){
        return persons.size()-1;
    }

    public long getNumInfoReq(){
        return courseInfoRequestLogs.size();
    }

    // normal getter/setter methods ==================================================================================


    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getDateStart() {
        return dateStart;
    }

    public void setDateStart(Date dateStart) {
        this.dateStart = dateStart;
    }

    public Date getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(Date dateEnd) {
        this.dateEnd = dateEnd;
    }

    public long getCourseRegistrationNum() {
        return courseRegistrationNum;
    }

    public void setCourseRegistrationNum(long courseRegistrationNum) {
        this.courseRegistrationNum = courseRegistrationNum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getNumEvaluationsCompleted() {
        return numEvaluationsCompleted;
    }

    public void setNumEvaluationsCompleted(long numEvaluationsCompleted) {
        this.numEvaluationsCompleted = numEvaluationsCompleted;
    }

    public Collection<RegistrationTimestamp> getTimeStamps() {
        return timeStamps;
    }

    public void setTimeStamps(Collection<RegistrationTimestamp> timeStamps) {
        this.timeStamps = timeStamps;
    }

    public Collection<Evaluation> getEvaluations() {
        return evaluations;
    }

    public void setEvaluations(Collection<Evaluation> evaluations) {
        this.evaluations = evaluations;
    }

    public Collection<Attendance> getAttendances() {
        return attendances;
    }

    public void setAttendances(Collection<Attendance> attendances) {
        this.attendances = attendances;
    }

    public Collection<Person> getPersons() {
        return persons;
    }

    public void setPersons(Collection<Person> persons) {
        this.persons = persons;
    }

    public Collection<CourseInfoRequestLog> getCourseInfoRequestLogs() {
        return courseInfoRequestLogs;
    }

    public void setCourseInfoRequestLogs(Collection<CourseInfoRequestLog> courseInfoRequestLogs) {
        this.courseInfoRequestLogs = courseInfoRequestLogs;
    }
}
