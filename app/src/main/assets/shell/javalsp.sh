apt install -y gnupg
apt update && apt install -y wget gpg ca-certificates

mkdir -p /etc/apt/keyrings
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg

echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print $2}' /etc/os-release) main" > /etc/apt/sources.list.d/adoptium.list

apt update
apt install -y temurin-25-jdk

java -version
javac -version

mkdir -p /root/jdtls
cd /root/jdtls
wget -q http://download.eclipse.org/jdtls/snapshots/jdt-language-server-latest.tar.gz -O jdtls.tar.gz
tar -xzf jdtls.tar.gz
rm jdtls.tar.gz
chmod +x /root/jdtls/bin/jdtls

ls /root/jdtls/bin/jdtls
java -version