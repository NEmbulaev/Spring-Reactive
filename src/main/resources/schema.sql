DROP TABLE PERSON IF EXISTS;

CREATE TABLE IF NOT EXISTS PERSON(
                            ID INT PRIMARY KEY AUTO_INCREMENT,
                            NAME VARCHAR(255),
                            SURNAME VARCHAR(255)
);
DROP TABLE COMPANY IF EXISTS;
CREATE TABLE IF NOT EXISTS COMPANY(
                                     ID INT PRIMARY KEY AUTO_INCREMENT,
                                     COMPANY_NAME VARCHAR(255)

    );

