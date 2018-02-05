#!/bin/bash

# This generates new BigQuery dataset for use in cloudsql by the workbench
# and dumps csvs of that dataset to import to cloudsql
# note account must be preauthorized with gcloud auth login

# End product is:
# 0) Big query dataset for cdr version cdrYYYYMMDD
# 1) .csv of all the tables in a bucket

# Example usage, you need to provide a bunch of args
# Provide:  your authorized gcloud account
#  bq project and dataset where the omop CDR release is
#  the workbench-project you want the new dataset  to be generated in
#  the cdr release number -- YYYYMMDD format . This is used to name generated datasets
#  the gcs bucket you want to put the generated data in
#
# ./project.rb generate-bigquery-cloudsql-cdr --account peter.speltz@pmi-ops.org --bq-project all-of-us-ehr-dev \
# --bq-dataset test_merge_dec26 --workbench-project all-of-us-workbench-test --cdr-version 20180130 --bucket all-of-us-workbench-cloudsql-create

set -xeuo pipefail
IFS=$'\n\t'

USAGE="./generate-cdr/generate-bigquery-cloudsql-cdr --bq-project <PROJECT> --bq-dataset <DATASET> --workbench-project <PROJECT> --public-project <PROJECT>"
USAGE="$USAGE --account <ACCOUNT> --cdr-version=YYYYMMDD"
USAGE="$USAGE \n Data is generated from bq-project.bq-dataset and dumped to workbench-project.cdr<cdr-version>."
USAGE="$USAGE \n Local mysql databases named cdr<cdr-version> and public<cdr-version> are created and populated."

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --account) ACCOUNT=$2; shift 2;;
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    --workbench-project) WORKBENCH_PROJECT=$2; shift 2;;
    --public-project) PUBLIC_PROJECT=$2; shift 2;;
    --cdr-version) CDR_VERSION=$2; shift 2;;
    --bucket) BUCKET=$2; shift;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done
# Todo this requires args in right order and doesn't print usage. Prints "Unbound variable ...."
if [ -z "${ACCOUNT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BQ_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BQ_DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${WORKBENCH_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${PUBLIC_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${CDR_VERSION}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BUCKET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

#Check cdr_version is of form YYYYMMDD
if [[ $CDR_VERSION =~ ^[0-9]{4}(0[1-9]|1[0-2])(0[1-9]|[1-2][0-9]|3[0-1])$ ]]; then
    echo "New CDR VERSION will be $CDR_VERSION"
  else
    echo "CDR Version doesn't match required format YYYYMMDD"
    echo "Usage: $USAGE"
    exit 1
fi

WORKBENCH_DATASET=cdr$CDR_VERSION
PUBLIC_DATASET=public$CDR_VERSION

## Make BigQuery dbs
echo "Making BigQuery dataset for CloudSql cdr"
if ./generate-cdr/make-bq-data.sh --bq-project $BQ_PROJECT --bq-dataset $BQ_DATASET --workbench-project $WORKBENCH_PROJECT --account $ACCOUNT --cdr-version $CDR_VERSION
then
    echo "BigQuery cdr data generated"
else
    echo "FAILED To generate BigQuery data for cdr $CDR_VERSION"
    exit 1
fi

# Make public
echo "Making BigQuery public dataset for CloudSql cdr"
if ./generate-cdr/make-bq-public-data.sh --workbench-project $WORKBENCH_PROJECT --workbench-dataset $WORKBENCH_DATASET --public-project $PUBLIC_PROJECT --public-dataset $PUBLIC_DATASET
then
    echo "BigQuery public cdr data generated"
else
    echo "FAILED To generate public BigQuery data for $CDR_VERSION"
    exit 1
fi

#dump workbench cdr counts
echo "Making big query dataset for cloudsql cdr"
if ./generate-cdr/make-bq-data-dump.sh --dataset $WORKBENCH_DATASET --project $WORKBENCH_PROJECT --account $ACCOUNT --bucket $BUCKET
then
    echo "Workbench cdr count data dumped"
else
    echo "FAILED to dump Workbench cdr count data"
    exit 1
fi

# dump public counts
dataset=cdr$CDR_VERSION
echo "Making big query dataset for cloudsql cdr"
if ./generate-cdr/make-bq-data-dump.sh --dataset $PUBLIC_DATASET --project $PUBLIC_PROJECT --account $ACCOUNT --bucket $BUCKET
then
    echo "Public cdr count data dumped"
else
    echo "FAILED to dump Public cdr count data"
    exit 1
fi