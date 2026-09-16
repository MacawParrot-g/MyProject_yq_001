package org.example.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportMessage implements Serializable {
    private String recorder;
    private String fileName;
    private long timestamp;
    private String mode;
    private String date;
    private java.util.List<String> hashes;
}
