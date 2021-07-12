# Glass - Loader
Basic code for loading shards and launching the game.

# Setup
Instructions should be relatively clear, but don't be afraid to ask because there is very likely to be something missing.

## Downloading
Download / Clone the github repository to get the contents locally.

## Running
Kiln does not have any functionality yet in terms of generating run configurations, so here is a manual tutorial.

- **Classpath:** {project-name}.exec.main
- **JVM Arguments:** see [jvm arguments](#jvm-arguments)
- **Main Class:** com.github.glassmc.loader.client.GlassClientMain
- **Program Arguments:** see [program arguments](#program-arguments)
- **Working Directory:** run

### JVM Arguments
To get the proper jvm arguments, run

`./gradlew getRunConfiguration -Penvironment={client/server} -Pversion={version}`

For example, to get the correct arguments for running a 1.8.9 client.

`./gradlew getRunConfiguration -Penvironment=client -Pversion=1.8.9`

You will see a long string printed into the terminal, copy that and add it to your jvm arguments.

### Program Arguments
Most versions will work with supplying

`--accessToken 0 --version {version}`

(for offline mode)

but some versions (at least 1.7.10) require also adding

`--userProperties {}`