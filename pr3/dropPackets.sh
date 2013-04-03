!#/bin/bash

if ! [[ "$1" =~ ^[0-9]+$ ]] ; then
   exec >&2; echo "error: Argument 1 is not a number"; exit 1
fi

echo "Setting rate of packet loss to $1 percent..."
sed -i "s/r >= -*[0-9]*/r >= $1/" src/edu/cmu/ece/backend/UDPManager.java