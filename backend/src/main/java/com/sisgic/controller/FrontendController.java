package com.sisgic.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class FrontendController {

    @GetMapping
    public String index() {
        return "forward:/index.html";
    }

    @GetMapping(value = {
        "/dashboard",
        "/publications",
        "/scientific-events",
        "/technology-transfer",
        "/postdoctoral-fellows",
        "/outreach-activities",
        "/scientific-collaborations",
        "/thesis-students",
        "/catalogs",
        "/researchers",
        "/projects",
        "/login",
        "/register"
    })
    public String forward() {
        return "forward:/index.html";
    }
}


