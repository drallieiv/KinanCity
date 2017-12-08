#!/usr/bin/env python
# -*- coding: utf-8 -*-

import tqdm
import codecs
import argparse
import requests

from bs4 import BeautifulSoup


def parseArgs():
    parser = argparse.ArgumentParser(description="Resend PTC accounts activation e-mail")
    parser.add_argument("file", help="KinanCity result file, that is, a csv with username;password;email")
    parser.add_argument("--sep", default=";", help="file separator, defaults to ;")

    args = parser.parse_args()

    return (args.file, args.sep)


def parseFile(inputFile, sep=";", commentaryMarker="#"):
    data = []

    with codecs.open(inputFile, 'r', 'utf-8') as f:
        for line in f:
            line = line.strip()
            if len(line) < 1 or line[0] == commentaryMarker:
                continue

            parts = line.split(sep)
            username = parts[0]
            password = parts[1]
            email = parts[2]

            data.append({"username": username, "password": password, "email": email})

    return data


def getTokenAndCookies(url="https://club.pokemon.com/us/pokemon-trainer-club/activated"):
    resendPage = requests.get(url)

    soup = BeautifulSoup(resendPage.text, 'html.parser')
    csrfMiddlewareTokenInput = soup.find(attrs={"name": "csrfmiddlewaretoken"})
    token = csrfMiddlewareTokenInput.attrs['value']

    return token, resendPage.cookies


def resendActivation(email, password, username, url="https://club.pokemon.com/us/pokemon-trainer-club/activated"):
    token, cookies = getTokenAndCookies()

    postData = {
        "csrfmiddlewaretoken": token,
        "email": email,
        "username": username,
        "password": password
    }

    postHeaders = {
        "authority": "club.pokemon.com",
        "referer": "https://club.pokemon.com/us/pokemon-trainer-club/activated"
    }

    requests.post(url, data=postData, headers=postHeaders, cookies=cookies)


def main():
    file, sep = parseArgs()
    accountsData = parseFile(file, sep=sep)

    for accountData in tqdm.tqdm(accountsData):
        resendActivation(accountData['email'], accountData['password'], accountData['username'])


if __name__ == '__main__':
    main()
