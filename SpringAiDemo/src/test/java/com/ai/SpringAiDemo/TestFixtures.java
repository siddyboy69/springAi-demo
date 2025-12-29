package com.ai.SpringAiDemo;

import com.ai.SpringAiDemo.domain.Allergy;
import com.ai.SpringAiDemo.domain.User;

import java.util.List;

public class TestFixtures {
    public static Allergy allergy(){
        return new Allergy("peanuts");
    }
    public static Allergy allergy2(){
        return new Allergy("gluten");
    }

    public static User user(){
        return User.builder().firstName("Siddhanta").lastName("Khadka").allergies(List.of(allergy(), allergy2())).build();
    }
}
