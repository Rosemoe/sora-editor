/*
 Copyright 2020 Rose2073

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.rose.editor.langs.msg;

/**
 * @author Rose
 *
 */
public class StringAdvice implements Advice {

    private String adv;

    public StringAdvice(String advice) {
        this.adv = advice;
    }

    public String getAdvice() {
        return adv;
    }

    public void setAdvice(String adv) {
        this.adv = adv;
    }

    @Override
    public String toString() {
        return "StringAdvice {" + adv + "}";
    }

}
