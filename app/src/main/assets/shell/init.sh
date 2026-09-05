#bin/shell

# download in server
rm -f /etc/resolv.conf
echo "nameserver 8.8.8.8" > /etc/resolv.conf

#install last node js and...
curl -fsSL https://deb.nodesource.com/setup_24.x | sudo -E bash -
curl -fsSL https://deb.nodesource.com/setup_lts.x | bash -
