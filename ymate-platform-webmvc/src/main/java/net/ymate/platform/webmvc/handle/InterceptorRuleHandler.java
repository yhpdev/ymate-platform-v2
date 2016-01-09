/*
 * Copyright 2007-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ymate.platform.webmvc.handle;

import net.ymate.platform.core.beans.IBeanHandler;
import net.ymate.platform.core.util.ClassUtils;
import net.ymate.platform.webmvc.IInterceptorRule;
import net.ymate.platform.webmvc.IWebMvc;

/**
 * @author 刘镇 (suninformation@163.com) on 16/1/8 下午4:48
 * @version 1.0
 */
public class InterceptorRuleHandler implements IBeanHandler {

    private IWebMvc __owner;

    public InterceptorRuleHandler(IWebMvc owner) {
        __owner = owner;
    }

    @SuppressWarnings("unchecked")
    public Object handle(Class<?> targetClass) throws Exception {
        if (ClassUtils.isInterfaceOf(targetClass, IInterceptorRule.class)) {
            __owner.registerInterceptorRule((Class<? extends IInterceptorRule>) targetClass);
        }
        return null;
    }
}