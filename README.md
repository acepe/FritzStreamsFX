# FritzStreamsFX
JavaFX app for downloading music streams from radio Fritz (www.fritz.de)

## Features
* Download streams from radio Fritz
* Display playlist (if available)
* Playlist is written to MP3 ID3 tags
* Integrated music player for downloaded files
* Modern JavaFX user interface
* Tagging and organizing downloaded music
* Search and filter streams

## Requirements
- Java 11 or newer
- Maven (for building from source)
- Internet connection

## Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/acepe/FritzStreamsFx.git
   ```
2. Build the project with Maven:
   ```bash
   mvn clean package
   ```
   The JAR file will be created in the `target/` directory.

## How to Run
You can run the JavaFX app using the generated JAR file:
```bash
java -jar target/FritzStreams.jar
```
Alternatively, you can run the app directly from your IDE (e.g., IntelliJ IDEA) by executing the `FritzStreamsApp` main class.

## Example Usage
- Start the app and select a stream from the list.
- Click the download button to save the stream to your local directory.
- View and play downloaded files in the integrated player.
- The playlist (if available) will be shown and written to the MP3 file's ID3 tags.

## StahlwerkDownloader

`StahlwerkDownloader` is a Java program for automated downloading and tagging of Stahlwerk streams. It can be used to automate downloading using a cronjob or similar scheduling tools.

### Usage

```bash
java -jar target/stahlwerkDL.jar <target-directory>
```

- `<target-directory>`: Optional. Path where the downloaded file will be saved. Defaults to the current working directory.

### Features

- Finds and downloads the current Stahlwerk stream.
- Shows progress and download status in the log.
- Writes playlist information as a comment in the MP3 file's ID3 tags.

### Requirements

- Java 11+
- Maven to build the project

### Example

```bash
java -jar stahlwerk-downloader.jar /home/user/Music
```

The downloaded file contains the playlist as a comment in the ID3 tag.

### Error Handling

- If the target directory is invalid or lacks write permissions, an error is logged.
- On download or tagging errors, the program exits with an error code.