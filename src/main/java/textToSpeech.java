// Imports the Google Cloud client library

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;
import javafx.beans.binding.StringBinding;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class textToSpeech {


    public static void main(String... args) throws Exception {
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {

            int parts = 8; //Разделить на сколько потоков

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
                        List<String> strings = splitText((String) objects[1], parts);
                        List<ByteString> audioContents = strings.parallelStream()
                                .map(textPart -> {
                                    SynthesisInput input = SynthesisInput.newBuilder().setText((String) objects[1]).build();
                                    SynthesizeSpeechResponse response =
                                            textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
                                    return response.getAudioContent();
                                })
                                .collect(Collectors.toList());
                        speechToFile(audioContents, (String) objects[0], encType);
                    });

        }
    }

    private static List<String> splitText(String text, int parts){
        List<String> result = new ArrayList<>();
        List<String> words = Arrays.asList(text.split(" "));
        int wordsInPart = words.size() / parts;
        IntStream.range(0, parts).forEach(i -> {
            int firstEdge = i * wordsInPart;
            int lastEdge = i < parts - 1
                    ? (i + 1) * wordsInPart
                    : words.size() - 1;
            result.add(String.join(" ", words.subList(firstEdge, lastEdge)));
        });
        return result;
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

    private static void speechToFile(List<ByteString> audioContents, String fileName, AudioEncoding encType)  {
        String newFileName = fileName + "."+encType.toString().toLowerCase();

        try (OutputStream out = new FileOutputStream(newFileName)) {
            try {
                final ByteString[] allAudioContent = {null};
                audioContents.forEach(audioContent -> {
                    if(allAudioContent[0] == null){
                        allAudioContent[0] = audioContent;
                    }else {
                        allAudioContent[0].concat(audioContent);
                    }

                });

                out.write(allAudioContent[0].toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
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