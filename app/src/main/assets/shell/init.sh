#bin/shell

# download in server
rm -f /etc/resolv.conf
echo "nameserver 8.8.8.8" > /etc/resolv.conf

if command -v node >/dev/null 2>&1; then
  echo "[GHOSTIDE] node already installed: $(node -v)"
  exit 0
fi

echo "[GHOSTIDE] installing node.js via nodesource..."
curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - && apt-get install -y nodejs

if ! command -v node >/dev/null 2>&1; then
  echo "[GHOSTIDE] nodesource failed, falling back to Debian apt package..."
  apt-get update
  apt-get install -y nodejs npm
fi

if command -v node >/dev/null 2>&1; then
  echo "[GHOSTIDE] node ready: $(node -v)"
else
  echo "[GHOSTIDE] ERROR: node could not be installed (check network/interfaces)."
  echo "[GHOSTIDE] Run manually inside Debian: apt-get update && apt-get install -y nodejs"
  exit 1
fi