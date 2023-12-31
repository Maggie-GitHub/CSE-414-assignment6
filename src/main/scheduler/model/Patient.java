package scheduler.model;

import scheduler.db.ConnectionManager;
import scheduler.util.Util;

import java.sql.*;
import java.util.Arrays;

public class Patient {
    private final String patientName;
    private final byte[] salt;
    private final byte[] hash;

    private Patient(PatientBuilder builder) {
        this.patientName = builder.patientName;
        this.salt = builder.salt;
        this.hash = builder.hash;
    }

    private Patient(PatientGetter getter) {
        this.patientName = getter.patientName;
        this.salt = getter.salt;
        this.hash = getter.hash;
    }

    // Getters
    public String getPatientName() {
        return patientName;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getHash() {
        return hash;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addPatient = "INSERT INTO Patients VALUES (?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addPatient);
            statement.setString(1, this.patientName);
            statement.setBytes(2, this.salt);
            statement.setBytes(3, this.hash);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException(e);
        } finally {
            cm.closeConnection();
        }
    }

    public static class PatientBuilder {
        private final String patientName;
        private byte[] salt;
        private byte[] hash;

        public PatientBuilder(String patientName, byte[] salt, byte[] hash) {
            this.patientName = patientName;
            this.salt = salt;
            this.hash = hash;
        }

        public Patient build() {
            return new Patient(this);
        }
    }

    public static class PatientGetter {
        private final String patientName;
        private final String password;
        private byte[] salt;
        private byte[] hash;

        public PatientGetter(String patientName, String password) {
            this.patientName = patientName;
            this.password = password;
        }

        public Patient get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getPatient = "SELECT * FROM Patients WHERE PatientName = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getPatient);
                statement.setString(1, this.patientName);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    byte[] salt = resultSet.getBytes("Salt");
                    // we need to call Util.trim() to get rid of the paddings,
                    // try to remove the use of Util.trim() and you'll see :)
                    byte[] hash = Util.trim(resultSet.getBytes("Hash"));
                    // check if the password matches
                    byte[] calculatedHash = Util.generateHash(password, salt);
                    if (!Arrays.equals(hash, calculatedHash)) {
                        return null;
                    } else {
                        this.salt = salt;
                        this.hash = hash;
                        return new Patient(this);
                    }
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }
}
