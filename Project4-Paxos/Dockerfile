FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the entire project directory
COPY . /app

# Compile the Java files
RUN find /app/src -name "*.java" > sources.txt
RUN javac @sources.txt -d /app/out

# Copy the hostsfile.txt to the output directory
COPY hostsfile-testcase1.txt /app/out/
COPY hostsfile-testcase2.txt /app/out/

# Set the working directory to the compiled classes
WORKDIR /app/out

# Run the main class
ENTRYPOINT ["java", "Main"]