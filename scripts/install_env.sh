# Update system
sudo apt update && sudo apt upgrade -y

# Install essential build tools
sudo apt install -y build-essential curl wget git vim pkg-config libssl-dev

# Configure Git (use same as Windows)
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"


# Install OpenJDK 17 (LTS)
sudo apt install -y openjdk-17-jdk

# Install Maven
sudo apt install -y maven

# Set JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

# Verify installation
java -version
mvn -version