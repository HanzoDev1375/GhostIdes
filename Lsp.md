# Lsp

## Frist install node js in terminal 

```bash
 curl -fsSL https://deb.nodesource.com/setup_lts.x | bash -
 apt install -y nodejs
 node -v
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

```


### Python Lsp 

- Frist install in Terminal 

```bash

pip install python-lsp-server

```

### JavaScript && ts jsx tsx Lsp

- Frist Install Node js and install TypeScript Lsp 

```bash

npm install -g typescript typescript-language-server

```

# Html Lsp 
# Css Lsp

```bash

npm install -g vscode-langservers-extracted

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