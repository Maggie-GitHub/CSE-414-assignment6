package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;

//the date should be format like "2022-02-02"

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        // create_patient <username> <password>
        if (tokens.length != 3) {
            System.out.println("Failed to create a patient user.");
            return;
        }
        String patientName = tokens[1];
        String password = tokens[2];
        // Patient name has been taken already
        if (usernameExistsPatient(patientName)) {
            System.out.println("Sorry, this patient name has been taken, please try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            Patient patient = new Patient.PatientBuilder(patientName, salt, hash).build();
            patient.saveToDB();
            System.out.println("Your created patient name" + patientName);
        } catch (SQLException e) {
            System.out.println("Failed to create a patient user name.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String patientName) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectPatientname = "SELECT * FROM Patients WHERE PatientName = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectPatientname);
            statement.setString(1, patientName);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Something wrong when checking username, please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create a caregiver user.");
            return;
        }
        String CareName = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(CareName)) {
            System.out.println("CareName taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(CareName, salt, hash).build();
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + CareName);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String CareName) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectCareName = "SELECT * FROM Caregivers WHERE CareName = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectCareName);
            statement.setString(1, CareName);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        // login_patient <username> <password>
        // if someone's already logged in
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("You are successfully logged in.");
            return;
        }
        // the length
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String patientName = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(patientName, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("You are logged in as: " + patientName);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String CareName = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(CareName, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + CareName);
            currentCaregiver = caregiver;
        }
    }
    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }

        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }

        String dateString = tokens[1];

        try {
            Date date = Date.valueOf(dateString);
            // Call a method to fetch and print caregiver schedules for the given date
            // Implement this method based on your database schema
            printCaregiverSchedules(date);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date format. Please enter a valid date!");
        }
    }

    private static void printCaregiverSchedules(Date date) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String caregiversQuery = "SELECT DISTINCT A.CareName " +
                "FROM Availabilities A " +
                "WHERE A.Time = ?";

        try {
            // Query for caregivers
            PreparedStatement caregiversStatement = con.prepareStatement(caregiversQuery);
            caregiversStatement.setDate(1, date);
            ResultSet caregiversResultSet = caregiversStatement.executeQuery();

            System.out.println("Available caregivers:");
            while (caregiversResultSet.next()) {
                String caregiverName = caregiversResultSet.getString("CareName");
                System.out.println(caregiverName);
            }
        } catch (SQLException e) {
            System.out.println("Please try again for the caregivers!");
            e.printStackTrace();
        }

        String vaccinesQuery = "SELECT V.VaccineName, V.Doses AS AvailableDoses " +
                "FROM Vaccines V " +
                "ORDER BY V.VaccineName";

        try {
            // Query for available doses per vaccine
            PreparedStatement vaccinesStatement = con.prepareStatement(vaccinesQuery);
            ResultSet vaccinesResultSet = vaccinesStatement.executeQuery();

            System.out.println("Available vaccines:");
            while (vaccinesResultSet.next()) {
                String vaccineName = vaccinesResultSet.getString("VaccineName");
                int availableDoses = vaccinesResultSet.getInt("AvailableDoses");
                System.out.println(vaccineName + " " + availableDoses);
            }
        } catch (SQLException e) {
            System.out.println("Please try again for the vaccines!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        if (currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        if (!(currentPatient instanceof Patient)) {
            System.out.println("Please login as a patient!");
            return;
        }

        String dateString = tokens[1];
        String vaccineName = tokens[2];

        try {
            Date date = Date.valueOf(dateString);

            // Call a method to fetch and print caregiver schedules for the given date and vaccine
            // Implement this method based on your database schema
            boolean success = reserveAppointment(date, vaccineName);
            if (success) {
                System.out.println("Appointment success!");
            } else {
                System.out.println("Please select another date or vaccine.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date format. Please enter a valid date!");
        }
    }

    private static boolean reserveAppointment(Date date, String vaccineName) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String caregiverQuery = "SELECT CareName " +
                                "FROM Availabilities " +
                                "WHERE Time = ?";

        String vaccineQuery = "SELECT Doses " +
                              "FROM Vaccines " +
                              "WHERE VaccineName = ? AND Doses > 0";

        String updateAvailabilities = "DELETE FROM Availabilities " +
                                      "WHERE Time = ? AND CareName = ?";

        String updateVaccines = "UPDATE Vaccines " +
                                "SET Doses = Doses - 1 " +
                                "WHERE VaccineName = ?";

        String insertReservation = "INSERT INTO Reservation (appointmentID, appointmentDate, CareName, PatientName, VaccineName) VALUES (?, ?, ?, ?, ?)";

        try {
            // Check for available caregivers
            PreparedStatement caregiverStatement = con.prepareStatement(caregiverQuery);
            caregiverStatement.setDate(1, date);
            ResultSet caregiverResultSet = caregiverStatement.executeQuery();

            if (caregiverResultSet.next()) {
                String CareName = caregiverResultSet.getString("CareName");

                // Check for available doses of the selected vaccine
                PreparedStatement vaccineStatement = con.prepareStatement(vaccineQuery);
                vaccineStatement.setString(1, vaccineName);
                ResultSet vaccineResultSet = vaccineStatement.executeQuery();

                if (vaccineResultSet.next()) {
                    // If both caregivers and vaccine are available, proceed with the reservation
                    PreparedStatement updateAvailabilitiesStatement = con.prepareStatement(updateAvailabilities);
                    updateAvailabilitiesStatement.setDate(1, date);
                    updateAvailabilitiesStatement.setString(2, CareName);
                    updateAvailabilitiesStatement.executeUpdate();

                    PreparedStatement updateVaccinesStatement = con.prepareStatement(updateVaccines);
                    updateVaccinesStatement.setString(1, vaccineName);
                    updateVaccinesStatement.executeUpdate();

                    // Generate a 4-digit appointmentID
                    int appointmentID = generateAppointmentID(con);

                    // Insert reservation record
                    PreparedStatement insertReservationStatement = con.prepareStatement(insertReservation);
                    insertReservationStatement.setInt(1, appointmentID);
                    insertReservationStatement.setDate(2, date);
                    insertReservationStatement.setString(3, CareName);
                    insertReservationStatement.setString(4, ((Patient) currentPatient).getPatientName());
                    insertReservationStatement.setString(5, vaccineName);
                    insertReservationStatement.executeUpdate();

                    System.out.println("Success! Appointment booked! Appointment ID: " + String.format("%04d", appointmentID));
                    System.out.println("Caregiver: " + CareName);
                    return true;
                } else {
                    System.out.println("There are caregivers available, but not enough available doses!");
                }

            } else {
                System.out.println("No caregivers available for the specified date.");
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return false;
    }

    private static int generateAppointmentID(Connection con) throws SQLException {
        String query = "SELECT MAX(appointmentID) FROM Reservation";
        try (PreparedStatement statement = con.prepareStatement(query)) {
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) + 1;
            }
        }
        return 1; // If no existing records, start from 1
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }

        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }

        try {
            int appointmentId = Integer.parseInt(tokens[1]);

            // cancel the appointment based on the appointment ID
            boolean success = cancelAppointmentById(appointmentId);

            if (success) {
                System.out.println("Appointment canceled successfully!");
            } else {
                System.out.println("Failed to cancel appointment. Please try again!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid appointment ID. Please enter a valid number.");
        }
    }

    private static boolean cancelAppointmentById(int appointmentId) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        //when cancel the appointment, the availablity of caregiver doesn't go back

        // Queries to retrieve information needed for cancellation
        String getAppointmentInfo = "SELECT VaccineName, CareName, appointmentDate " +
                "FROM Reservation " +
                "WHERE appointmentID = ?;";

        String updateAvailabilities = "UPDATE Availabilities " +
                "SET CareName = NULL " +
                "WHERE Time = ? AND CareName = ?;";

        String updateVaccines = "UPDATE Vaccines " +
                "SET Doses = Doses + 1 " +
                "WHERE VaccineName = ?;";

        try {
            // Retrieve appointment information
            PreparedStatement getInfoStatement = con.prepareStatement(getAppointmentInfo);
            getInfoStatement.setInt(1, appointmentId);
            ResultSet infoResultSet = getInfoStatement.executeQuery();

            if (infoResultSet.next()) {
                String vaccineName = infoResultSet.getString("VaccineName");
                String careName = infoResultSet.getString("CareName");
                Date appointmentDate = infoResultSet.getDate("appointmentDate");

                // Cancel the appointment in the Reservation table
                String cancelAppointmentQuery = "DELETE FROM Reservation WHERE appointmentID = ?;";
                PreparedStatement cancelAppointmentStatement = con.prepareStatement(cancelAppointmentQuery);
                cancelAppointmentStatement.setInt(1, appointmentId);
                int rowsAffected = cancelAppointmentStatement.executeUpdate();

                if (rowsAffected > 0) {
                    // Update Availabilities table to mark the slot as available
                    PreparedStatement updateAvailabilitiesStatement = con.prepareStatement(updateAvailabilities);
                    updateAvailabilitiesStatement.setDate(1, appointmentDate);
                    updateAvailabilitiesStatement.setString(2, careName);
                    updateAvailabilitiesStatement.executeUpdate();

                    // Update Vaccines table to increment the available doses
                    PreparedStatement updateVaccinesStatement = con.prepareStatement(updateVaccines);
                    updateVaccinesStatement.setString(1, vaccineName);
                    updateVaccinesStatement.executeUpdate();

                    return true;
                }
            }

            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            cm.closeConnection();
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            // Check if the user is a patient or a caregiver
            if (currentPatient != null) {
                if (!printPatientAppointments(con, ((Patient) currentPatient).getPatientName())) {
                    System.out.println("No appointments found for the patient.");//if no appointment
                }
            } else if (currentCaregiver != null) {
                if (!printCaregiverAppointments(con, ((Caregiver) currentCaregiver).getCareName())) {
                    System.out.println("No appointments found for the caregiver.");//no appointment
                }
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }
    private static boolean printPatientAppointments(Connection con, String patientName) throws SQLException {
        String query = "SELECT * " +
                "FROM Reservation " +
                "WHERE PatientName = ? " +
                "ORDER BY appointmentID;";

        try (PreparedStatement statement = con.prepareStatement(query)) {
            statement.setString(1, patientName);
            ResultSet resultSet = statement.executeQuery();

            boolean appointmentsFound = false;

            while (resultSet.next()) {
                appointmentsFound = true;
                int appointmentID = resultSet.getInt("appointmentID");
                String vaccineName = resultSet.getString("VaccineName");
                Date appointmentDate = resultSet.getDate("appointmentDate");
                String CareName = resultSet.getString("CareName");

                StringBuilder output = new StringBuilder();
                output.append(appointmentID).append(" ")
                        .append(vaccineName).append(" ")
                        .append(appointmentDate).append(" ")
                        .append(CareName);

                System.out.println(output.toString());
            }
            return appointmentsFound;
        }
    }

    private static boolean printCaregiverAppointments(Connection con, String caregiverName) throws SQLException {
        String query = "SELECT * " +
                "FROM Reservation " +
                "WHERE CareName = ? " +
                "ORDER BY appointmentID;";

        try (PreparedStatement statement = con.prepareStatement(query)) {
            statement.setString(1, caregiverName);
            ResultSet resultSet = statement.executeQuery();

            boolean appointmentsFound = false;

            while (resultSet.next()) {
                appointmentsFound = true;
                int appointmentID = resultSet.getInt("appointmentID");
                String vaccineName = resultSet.getString("VaccineName");
                Date appointmentDate = resultSet.getDate("appointmentDate");
                String patientName = resultSet.getString("PatientName");

                StringBuilder output = new StringBuilder();
                output.append(appointmentID).append(" ")
                        .append(vaccineName).append(" ")
                        .append(appointmentDate).append(" ")
                        .append(patientName);

                System.out.println(output.toString());
            }

            return appointmentsFound;
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first.");
            return;
        }

        // Log out the current user
        currentPatient = null;
        currentCaregiver = null;

        System.out.println("Successfully logged out!");
    }
}
