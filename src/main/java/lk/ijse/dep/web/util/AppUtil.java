/**
 * MIT License
 * <p>
 * Copyright (c) 2020 Dhanusha Perera
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * @author : Dhanusha Perera
 * @author : Dhanusha Perera
 * @author : Dhanusha Perera
 * @since : 16/01/2021
 * @since : 16/01/2021
 * @since : 16/01/2021
 **/
/**
 * @author : Dhanusha Perera
 * @since : 16/01/2021
 **/
package lk.ijse.dep.web.util;

import java.io.IOException;
import java.util.Properties;

public class AppUtil {

    private static Properties properties;

    AppUtil() {

    }

    private static Properties getProperties() {
        return (properties == null) ? properties = new Properties() : properties;
    }

    public static String getValueByKey(String key) {
        Properties prop = getProperties();
        try {
            prop.load(AppUtil.class.getResourceAsStream("/application.properties"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return prop.getProperty(key);
    }
}
