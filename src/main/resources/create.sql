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


create table Reservation (
    CareName varchar(255) REFERENCES Caregivers,
    PatientName varchar(255) references Patients,
    VaccineName varchar(255) references Vaccines
);