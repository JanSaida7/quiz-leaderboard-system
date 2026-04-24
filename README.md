# Quiz Leaderboard System

## Internship Assignment Submission
Submitted for: Bajaj Finserv Health Limited – JAVA Qualifier | SRM | 24 Apr

## Candidate Details
- Name: SYED JAN SAIDA
- Roll Number: RA2311003010093
- Email: jansaidasyed743@gmail.com

---

## Problem Statement

Build a Java application that consumes quiz event data from the validator API, processes duplicate responses correctly, aggregates participant scores, generates a leaderboard, and submits the final result.

---

## Objective

The system should:

1. Poll the validator API 10 times using poll values from 0 to 9
2. Maintain a mandatory 5-second delay between each request
3. Collect all quiz events
4. Remove duplicate records using:

```text
(roundId + participant)