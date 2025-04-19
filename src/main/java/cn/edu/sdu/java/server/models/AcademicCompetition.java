package cn.edu.sdu.java.server.models;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Entity
@Table(	name = "academicCompetition",
        uniqueConstraints = {
        })

public class AcademicCompetition {
    //
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "student_id", referencedColumnName = "personId")
    @JsonIgnore
    private Student student;

    @Size(max = 20)
    @Column(name = "time")
    private LocalDate time;

    @Size(max = 20)
    @Column(name = "subject")
    private String subject;

    @Size(max = 50)
    @Column(name = "achievement")
    private String achievement;
}
