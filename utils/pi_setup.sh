sudo apt update
sudo apt upgrade
sudo apt install git python3 python3-pip python3-jupyterlab python3-pandas

# add these files manually
chmod 600 .ssh/id_ed25519
chmod 644 .ssh/id_ed25519.pub

git config --global user.email "kristo.isberg@gmail.com"
git config --global user.name "Kristo Isberg"

git clone git@github.com:kristoisberg/masters-thesis.git
cd masters-thesis

python -m jupyterlab --ip 0.0.0.0 --no-browser --NotebookApp.token='' --NotebookApp.password=''
