#cloud-config
users:
  - name: test
    shell: /bin/bash
    sudo: ALL=(ALL) NOPASSWD:ALL
    lock-passwd: false
    ssh-import-id: test
    ssh-authorized-keys:
      - ssh-rsa 
AAAAB3NzaC1yc2EAAAADAQABAAABAQCWpAc7qSNv7vA7yBMCFXBVTpwrdAXQPPJ7L0xuFd8lbnsMhP1aTtfeMqjOdyafybdkC6ZryamWtzbdvx4I4/zEQnTb1+Ls2OARM5iAh62xdvXZeq9mhzEZgnw+UIsAedvg9vyQz+NOSBg3XpwHDP76PEslN6CXoOAlJu5rFRpoPw1ysVbQPghDp6ceD5rEN4QInN9AhV5OFj6SDyypD8JPPCmBksNWLXsZEr+uuDSLh4VrCcrMSymtb2zDQreHe119sGJwvwTh8jWm+FRx6UITUl8y0LPPbUPevMtGaiHuL1i5T4Yj7loF/mG+StOsH6Xn+xhyMdZrfJfRAZkGxUaz 
root@test
