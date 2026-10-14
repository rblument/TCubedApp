//Edited by Dylan Cassagnol for initial sprint, week two

/*
 *  T^3: TCubed Task Tracking Tool
 * 
 *   (C) Richard Blumenthal, All rights reserved
 * 
 *   Unauthorized use, duplication or distribution without the authors'
 *   permission is strictly prohibited.
 * 
 *   Unless required by applicable law or agreed to in writing, this
 *   software is distributed on an "AS IS" basis without warranties
 *   or conditions of any kind, either expressed or implied.
 */

// Thomas Wintenburg - Initial Sprint
// Oscar Castillo - Initial Sprint Setup

package edu.regis.tcubed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.sql.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;


/**
 * Main entry point for the TCubed Task Tracking Tool Spring Boot application.
 * Bootstraps the application context and embedded web server.
 *
 * @author Oscar Castillo Saucedo
 */
@SpringBootApplication
public class TCubedApp {

    /**
     * Launches the TCubed Spring Boot application.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(TCubedApp.class, args);
    }
}

