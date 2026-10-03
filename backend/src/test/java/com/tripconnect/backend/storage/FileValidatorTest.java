package com.tripconnect.backend.storage;

import com.tripconnect.backend.exception.InvalidFileException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidatorTest {

    // Vài byte đầu ("magic bytes") đặc trưng của từng loại file
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0};
    private static final byte[] PDF = "%PDF-1.4\n%âãÏÓ\n1 0 obj\n".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0, (byte) 0xFF, (byte) 0xFF, 0, 0};

    private final FileValidator validator = new FileValidator();

    @Test
    void acceptsPngAsImage() {
        assertThat(validator.validate(file("tour.png", "image/png", PNG), FileRule.IMAGE))
                .isEqualTo("image/png");
    }

    @Test
    void acceptsJpegAsImage() {
        assertThat(validator.validate(file("tour.jpg", "image/jpeg", JPEG), FileRule.IMAGE))
                .isEqualTo("image/jpeg");
    }

    @Test
    void acceptsPdfAsDocument() {
        assertThat(validator.validate(file("giay-phep.pdf", "application/pdf", PDF), FileRule.DOCUMENT))
                .isEqualTo("application/pdf");
    }

    @Test
    void rejectsPdfWhenOnlyImagesAllowed() {
        assertThatThrownBy(() -> validator.validate(file("anh.pdf", "application/pdf", PDF), FileRule.IMAGE))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Định dạng file không hợp lệ");
    }

    @Test
    void rejectsExecutableDisguisedAsPdf() {
        // Tên và Content-Type đều giả là PDF, nhưng nội dung thật là file .exe
        assertThatThrownBy(() -> validator.validate(file("cccd.pdf", "application/pdf", EXE), FileRule.DOCUMENT))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Định dạng file không hợp lệ");
    }

    @Test
    void rejectsFileLargerThanLimit() {
        byte[] tooLarge = Arrays.copyOf(PNG, (int) FileRule.IMAGE.maxBytes() + 1);
        assertThatThrownBy(() -> validator.validate(file("big.png", "image/png", tooLarge), FileRule.IMAGE))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("dung lượng");
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> validator.validate(file("empty.png", "image/png", new byte[0]), FileRule.IMAGE))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("trống");
    }

    private static MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }
}
