# Build & Deploy

## Compile
mvn clean package -DskipTests

## Artifact
`target/CatppuccinStyler-1.0.0.jar`

## Deploy
```bash
scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null target/CatppuccinStyler-1.0.0.jar Servers:/tmp/CatppuccinStyler-1.0.0.jar
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null Servers "sudo mv /tmp/CatppuccinStyler-1.0.0.jar /var/lib/pterodactyl/volumes/3b43f1fd-f026-44d0-887d-1d980f4f6640/plugins/CatppuccinStyler-1.0.0.jar && sudo chown pterodactyl:pterodactyl /var/lib/pterodactyl/volumes/3b43f1fd-f026-44d0-887d-1d980f4f6640/plugins/CatppuccinStyler-1.0.0.jar"
```

## Restart (only if user requests)
Sends `/stop` to the server console for a graceful shutdown. Pterodactyl auto-restart brings it back up.
```bash
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null Servers "sudo docker exec 3b43f1fd-f026-44d0-887d-1d980f4f6640 sh -c 'echo stop > /proc/1/fd/0'"
```
If the above does not work, use the SIGTERM fallback:
```bash
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null Servers "sudo docker stop 3b43f1fd-f026-44d0-887d-1d980f4f6640"
```
