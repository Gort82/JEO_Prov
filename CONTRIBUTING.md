# Contributing

Thanks for considering a contribution!

## Development
- Java 17+
- Maven 3.9+

## Style
- Keep the implementation readable and aligned to the paper steps.
- Prefer small, well-named methods that map to algorithms/figures.

## Running
```bash
mvn test
mvn -DskipTests package
java -jar target/eowm-java-0.2.0.jar demo
```

## Reporting issues
Please include:
- command used
- expected vs actual behavior
- stack trace (if any)
- JVM + OS versions
