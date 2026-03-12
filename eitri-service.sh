#!/bin/sh
set -eu

exec java -cp /app/eitri.jar no.ntnu.eitri.service.EitriServiceMain "$@"
