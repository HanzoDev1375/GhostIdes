# In the name of God


## server language (lsp)

# To install the server language, do the following steps in order


## If you need web side languages such as html js, be sure to install the latest version of node first

- Note that if you receive an installation error in the packages, it is your own proxy 
- Set to 8.8.8.8
- With the help of this code

```bash

rm -f /etc/resolv.conf
echo "nameserver 8.8.8.8" > /etc/resolv.conf

```
- Just copy the code above to install the packages on the Iranian server side

```bash
 curl -fsSL https://deb.nodesource.com/setup_24.x | sudo -E bash -
curl -fsSL https://deb.nodesource.com/setup_lts.x | bash -
 apt install -y nodejs
 node -v
 apt update && apt upgrade -y
 npm install -g typescript ts-node
 npm install -g prettier
 
```
# This section is for php server language, if you haven't installed node, be sure to install it

### Php Lsp 

- install from terminal 

```bash

npm cache clean --force

npm install -g intelligence

```
# This section is for the language of the cpp server side, it is not supported in the future

### Cpp Lsp 

- install from terminal 

```bash

apt update && apt install -y clangd

apt install clang-format astyle -y

```
# This section is for Python, the favorite language of Iranian programmers

- In Python, it is better to copy and paste the instructions one by one so that there is no problem with the installation, thank you

### Python Lsp 

- First install in Terminal 

```bash

apt update && apt install python3-pip -y

pip install "python-lsp-server[all]" --break-system-packages

pip install ruff --break-system-packages

```

# This part is also for the web language and everything is clear, this also requires nodejs


### JavaScript && ts jsx tsx Lsp

- First install Node js and install TypeScript Lsp 

```bash

npm i -g typescript typescript-language-server

```
# This section also needs nodejs

## Html Css Json Markdown

```bash

npm i -g @t1ckbase/vscode-langservers-extracted

npm i -g @olrtg/emmet-language-server

```
# This section is for the fast go language, it may take a long time to install on some devices

- Note that installation is difficult depending on the strength of your device and note

## Go Lsp 

```bash
apt update && apt install -y golang-go


go install golang.org/x/tools/gopls@v0.14.2

```
# This section is designed for those who work with sass, it requires nodejs

## Sass Lsp

```bash
npm install -g some-sass-language-server

```
# As is clear, this section is for Ruby, which takes a little time to install

- Ruby installation may take between 2 and 20 minutes

## Ruby Lsp 

```bash

apt update
apt install -y ruby ruby-dev build-essential

ruby --version
gem --version

gem install solargraph
gem install rufo

```
# This section is being tested and may not work on some mobiles

## Charp Lsp 

```bash

apt update
apt install mono-complete -y
wget https://github.com/OmniSharp/omnisharp-roslyn/releases/latest/download/omnisharp-linux-arm64.tar.gz
mkdir -p omnisharp && tar -xzf omnisharp-linux-arm64.tar.gz -C omnisharp
chmod +x omnisharp/run

```

## Vue Lsp 

- try to install node.js

```bash

npm install -g typescript @vue/language-server

```

## Java Lsp 

```bash

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

```
