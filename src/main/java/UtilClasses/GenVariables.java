/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UtilClasses;

import java.nio.charset.Charset;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class GenVariables {

    public static final String UTF8_NAME = "UTF-8";
    
    public static final String ISO_NAME = "ISO-8859-1";
    
    public static final Charset UTF8 = Charset.forName(UTF8_NAME);

    public static final Charset ISO = Charset.forName(ISO_NAME);
}
