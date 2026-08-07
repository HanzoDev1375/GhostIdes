# به نام خداوند جان خرد


## زبان سرور (lsp)

# برای نصب زبان سرور اقدامات زیر را انجام بدهید به ترتیب


## اگر زبان های سمت وب مانند html js نیاز دارید ابتدا اخرین ورژن node را نصب کنید حتما

- توجه کنید اگر خطای نصب در پکیچ ها دریافت کردید پروکسی خودتان 
- تنظیم کنید روی 8.8.8.8
- با کمک این کد

```shell

rm -f /etc/resolv.conf
echo "nameserver 8.8.8.8" > /etc/resolv.conf

```
- فقط کد بالا را کپی کنید تا پکیچ ها سمت سرور ایران نصب شود

```bash
 curl -fsSL https://deb.nodesource.com/setup_24.x | sudo -E bash -
 curl -fsSL https://deb.nodesource.com/setup_lts.x | bash -
 apt install -y nodejs
 node -v
 apt update &&  apt upgrade -y
 npm install -g typescript ts-node
 npm install -g prettier
 
```
# این بخش برای زبان سرور سمت php هست اگر node نصب نکردید حتما نصب کنید

### Php Lsp 

- install from terminal 

```bash

npm cache clean --force

npm install -g intelephense

```
# این بخش برای زبان سمت سرور cpp هست فلن c ساپورت نمیشه در اینده

### Cpp Lsp 

- install from terminal 

```bash

apt update && apt install -y clangd

apt install clang-format astyle -y

```
# این بخش برای پایتون هست زبان مورد علاقه برنامه نویس های ایرانی

- در پایتون دستورات رو بهتره تک تک کپی کنید و پیست کنید تا مشکلی در نصب پیش نیاد ممنون

### Python Lsp 

- Frist install in Terminal 

```bash

apt update && apt install python3-pip -y

pip install "python-lsp-server[all]" --break-system-packages

pip install ruff --break-system-packages

```

# این قسمت هم برای زبان وب هست و همه چیز مشخص این هم نیازمند nodejs هست


### JavaScript && ts jsx tsx Lsp

- Frist Install Node js and install TypeScript Lsp 

```bash

npm i -g typescript typescript-language-server

```
# این بخش هم همین جور نیازمند nodejs هست

## Html Css Json Markdown

```bash

npm i -g @t1ckbase/vscode-langservers-extracted

npm i -g @olrtg/emmet-language-server

```
# این بخش برای زبان سریع go هست ممکنه در برخی از دستگاه ها نصبش طولانی بشه

- توجه فقط نصب سخت است بسته به قدرت دستگاه و نت شما دارد

## Go Lsp 

```bash
apt update && apt install -y golang-go


go install golang.org/x/tools/gopls@v0.14.2

```
# این بخش برای کسانی که sass کار میکنند طراحی شده است نیازمند nodejs هست

## Sass Lsp

```bash
npm install -g some-sass-language-server

```
# همان جور که مشخص است این بخش برای روبی است که نصب ان کمی طول میکشد

- نصب روبی ممکنه است بین 2 تا 20 دقیقه متغیر طول بکشد

## Ruby Lsp 

```bash

apt update
apt install -y ruby ruby-dev build-essential

ruby --version
gem --version

gem install solargraph
gem install rufo

```
# این بخش در حال تست است و ممکن است در برخی از موبایل ها کار نکند

## Charp Lsp 

```bash

apt update
apt install mono-complete -y
wget https://github.com/OmniSharp/omnisharp-roslyn/releases/latest/download/omnisharp-linux-arm64.tar.gz
mkdir -p omnisharp && tar -xzf omnisharp-linux-arm64.tar.gz -C omnisharp
chmod +x omnisharp/run

```


## Vue Lsp 

- ابتدا نود جی اس را نصب کنید

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
