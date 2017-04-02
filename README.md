# KinanCity

[![Last Version](https://img.shields.io/badge/version-1.0.0--Alpha1-brightgreen.svg)](https://github.com/drallieiv/KinanCity/releases/latest)
[![Build Status](https://travis-ci.org/drallieiv/KinanCity.svg?branch=master)](https://travis-ci.org/drallieiv/KinanCity)

Any issues with KinanCity, or you just want to talk about the project ? Go to [our discord server]( http://discord.gg/3jkb3zA)

## Were is Kinan City ?

**Kinan City** (キナンシティ) is one of the cities of the Kalos region in Pokemon XY games and anime.
It is known as **Kiloude City** is the English version and **Batisques** in French.

Kinan City is known for it **Friend Safari** where many trainer comes to find pokemon. This is a good place if you want to meet a **lot of new trainers**.

## What does KinanCity do ?

**KinanCity** is a tool that automates the creation of Pokemon Trainer Accounts and contains multiple modules.

- **KinanCity-core** : is the core module that can also be used in command line. [more info here](KinanCity-core/README.md)
- **KinanCity-mail** : is a minimalist Email server that does auto-activation. [more info here](KinanCity-mail/README.md)

## Why another tool ?

There are already many accout creator with each their specific features.  
KinanCity was born by taking the best features of them to have a complete solution.

The advantages of KinanCity :
* is cross platform compatible (Unix, Windows, Mac)
* can work on headless systems without any need of a web driver
* does parallel processing to be faster
* can use multiple proxies to go over the limit of 30 accounts per hour (5 accounts per 10 minutes)

## How to install KinanCity ?

You can **download** the latest builds [here](https://github.com/drallieiv/KinanCity/releases)  
or  
You can **compile** it yourself using a **maven** goal : `mvn package`  

**KinanCity** modules are java applications and all you need is a Java 8 Runtime Environment on your system.

### References

KinanCity is born from previous projects :

* [pikaptcha](https://github.com/sriyegna/Pikaptcha) by [sriyegna](https://github.com/sriyegna)
* [PalletTown](https://github.com/novskey/PalletTown) by [novskey](https://github.com/novskey)
