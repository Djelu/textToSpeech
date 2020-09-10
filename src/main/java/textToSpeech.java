// Imports the Google Cloud client library

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;


public class textToSpeech {


    public static void main(String... args) throws Exception {
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {

            //Форматы входа и выхода
            Charset charset = StandardCharsets.UTF_8;
            AudioEncoding encType = AudioEncoding.MP3;

            //Директория текстов
            String rootFolder = "C:\\Users\\Djelu\\Desktop\\333";

            //Настройки голоса
            VoiceSelectionParams voice =
                    VoiceSelectionParams.newBuilder()
                            .setLanguageCode("ru-RU")
                            .setSsmlGender(SsmlVoiceGender.FEMALE)
                            .setName("ru-RU-Wavenet-C")
                            .build();

            //Остальные настройки
            AudioConfig audioConfig =
                    AudioConfig.newBuilder()
                            .setAudioEncoding(encType)
                            .setSpeakingRate(1.75)//Скорость
                            .setPitch(1.5)
                            .build();

            Files.walk(Paths.get(rootFolder))
                    .filter(Files::isRegularFile)
                    .map(path -> new Object[]{getNameWithoutExt(path.toAbsolutePath().toString()), textToString(path, charset)})
                    .filter(objects -> Arrays.stream(objects).allMatch(Objects::nonNull))
                    .forEach(objects -> {
                        SynthesisInput input = SynthesisInput.newBuilder().setText((String) objects[1]).build();
                        SynthesizeSpeechResponse response =
                                textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
                        ByteString audioContents = response.getAudioContent();
                        speechToFile(audioContents, (String) objects[0], encType);
                    });
        }
    }

    private static String textToString(Path path, Charset charset) {
        try {
            return Files.lines(path.toAbsolutePath(), charset)
                    .collect(Collectors.joining(" "));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void speechToFile(ByteString audioContents, String fileName, AudioEncoding encType) {
        String newFileName = fileName + "."+encType.toString().toLowerCase();

        try (OutputStream out = new FileOutputStream(newFileName)) {
            out.write(audioContents.toByteArray());
            System.out.println(String.format("Audio content written to file \"%s\"", newFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getNameWithoutExt(String name) {
        int i = name.lastIndexOf(".");
        return i > -1 ? name.substring(0, i) : null;
    }
}