# survivability-sim
Simulator that creates and evaluates solutions for survivability problems.


# Building
### Preparing Your Environment

Make sure the following are installed on your system:

* [Java](https://www.java.com) 1.8
* [Maven](http://maven.apache.org) 3.1+

### Setting config variables
If using AWS for storage/analysis of results, include values for all fields in the config/application.properties file.
```bash
aws_access_key_id={your access key ID}
aws_secret_access_key={your secret key}
aws_region={which region survivability-sim should connect to}
aws_role_arn={ARN for the role should survivability-sim use}
aws_role_session_name={name of the role}
aws_meta_db={name of database used to store metadata}
aws_raw_bucket={name of the S3 bucket for storing raw data}
aws_analyzed_bucket={name of the S3 bucket for storing analyzed data}
```
### Inlcuding the AMPL license
Put your license file (**ampl.lic**) for running AMPL in the linear-programs/ampl/ directory.

### Installing the AMPL API jar
Run the following maven command to include the AMPL API in your local repository:
```bash
mvn install:install-file -Dfile={your-path-to-survivability-sim}/linear-programs/ampl/ampl-1.3.1.0.jar -DgroupId=com.ampl -DartifactId=ampl -Dversion=1.3.1.0 -Dpackaging=jar
```


### Building using maven

Run the following commands from the main project directory (survivability-sim):

```
mvn -DskipTests install
```

## Testing survivability-sim
You can run the unit tests with the command:

```bash
mvn test
```

You may also install only if the tests pass by running:

```bash
mvn install
