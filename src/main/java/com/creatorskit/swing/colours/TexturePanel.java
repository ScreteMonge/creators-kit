package com.creatorskit.swing.colours;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

@AllArgsConstructor
@Getter
@Setter
public class TexturePanel extends JPanel
{
    private boolean textureSet;
    private short oldTexture;
    private short newTexture;
    private JLabel oldTextureLabel;
    private JButton newTextureButton;
    private JSpinner spinner;
}
