package com.ai.SpringAiDemo.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.jpa.domain.AbstractPersistable;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder

@Entity
@Table(name = "users")
public class User extends AbstractPersistable<Long> {
    @NotNull
    String firstName;
    @NotNull
    String lastName;
    @ElementCollection
    @JoinTable(foreignKey = @ForeignKey(name = "FK_user_2_allergy"))
    @NotNull
    List<Allergy> allergies;
}
