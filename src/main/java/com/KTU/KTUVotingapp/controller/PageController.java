package com.KTU.KTUVotingapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String landing() {
        return "landing";
    }

    @GetMapping("/pin")
    public String pin() {
        return "pin";
    }

    @GetMapping("/king-selection")
    public String kingSelection() {
        return "king-selection";
    }

    @GetMapping("/queen-selection")
    public String queenSelection() {
        return "queen-selection";
    }

    @GetMapping("/prince-selection")
    public String princeSelection() {
        return "prince-selection";
    }

    @GetMapping("/princess-selection")
    public String princessSelection() {
        return "princess-selection";
    }

    @GetMapping("/couple-selection")
    public String coupleSelection() {
        return "couple-selection";
    }

    @GetMapping("/summary")
    public String summary() {
        return "card-layout";
    }

    @GetMapping("/success")
    public String success() {
        return "success";
    }
}


