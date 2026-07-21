# Lsp

## Frist install node js in terminal 

```bash
 curl -fsSL https://deb.nodesource.com/setup_24.x | sudo -E bash -
 apt install -y nodejs
 node -v
 apt update &&  apt upgrade -y
 npm install -g typescript ts-node
 npm install -g prettier
 
```

### Php Lsp 

- install from terminal 

```bash

npm cache clean --force

npm install -g intelephense

```

### Cpp Lsp 

- install from terminal 

```bash

apt update && apt install -y clangd

apt install clang-format astyle -y

```


### Python Lsp 

- Frist install in Terminal 

```bash

apt update && apt install python3-pip -y

pip install "python-lsp-server[all]" --break-system-packages

pip install ruff --break-system-packages

```

### JavaScript && ts jsx tsx Lsp

- Frist Install Node js and install TypeScript Lsp 

```bash

npm i -g typescript typescript-language-server

```

# Html Css Json Markdown

```bash

npm i -g @t1ckbase/vscode-langservers-extracted

npm i @olrtg/emmet-language-server

```

# Go Lsp 

```bash
apt update && apt install -y golang-go


go install golang.org/x/tools/gopls@v0.14.2

```

# Sass Lsp

```bash
npm install -g some-sass-language-server

```

# Ruby Lsp 

```bash

apt update
apt install -y ruby ruby-dev build-essential

ruby --version
gem --version

gem install solargraph
gem install rufo

```

# Charp Lsp 

```bash

apt update
apt install mono-complete -y
wget https://github.com/OmniSharp/omnisharp-roslyn/releases/latest/download/omnisharp-linux-arm64.tar.gz
mkdir -p omnisharp && tar -xzf omnisharp-linux-arm64.tar.gz -C omnisharp
chmod +x omnisharp/run

```