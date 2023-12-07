CREATE TABLE Caregivers (
    CareName varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (CareName)
);

CREATE TABLE Availabilities (
    Time date,
    CareName varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, CareName)
);

CREATE TABLE Vaccines (
    VaccineName varchar(255),
    Doses int,
    PRIMARY KEY (VaccineName)
);

create table Patients (
    PatientName VARCHAR(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (PatientName)
);


CREATE TABLE Reservation (
    appointmentID INT PRIMARY KEY,
    appointmentDate DATE,
    CareName VARCHAR(255) REFERENCES Caregivers,
    PatientName VARCHAR(255) REFERENCES Patients,
    VaccineName VARCHAR(255) REFERENCES Vaccines
);