apt update
apt install mono-complete -y
wget https://github.com/OmniSharp/omnisharp-roslyn/releases/latest/download/omnisharp-linux-arm64.tar.gz
mkdir -p omnisharp && tar -xzf omnisharp-linux-arm64.tar.gz -C omnisharp
chmod +x omnisharp/run