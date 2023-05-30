/*
 *
 *  * Copyright 2018 Johns Hopkins University
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.eclipse.pass.notification.dispatch.email;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.apache.commons.io.input.NullInputStream;
import org.mockito.InOrder;

public class CompositeResolverTest {

//    private CompositeResolver underTest;
//
//    @Before
//    public void setUp() throws Exception {
//        // TODO use spring to autowire
//        SpringUriTemplateResolver springUriTemplateResolver = new SpringUriTemplateResolver();
//        InlineTemplateResolver inlineTemplateResolver = new InlineTemplateResolver();
//        underTest = new CompositeResolver(springUriTemplateResolver, inlineTemplateResolver);
//    }
//
//    @Test
//    public void resolutionOrder() {
//        TemplateResolver one = mock(TemplateResolver.class);
//        TemplateResolver two = mock(TemplateResolver.class);
//        TemplateResolver three = mock(TemplateResolver.class);
//        resolvers.add(one);
//        resolvers.add(two);
//        resolvers.add(three);
//
//        String template = "a template";
//
//        when(three.resolve(null, template)).thenReturn(new NullInputStream(-1L));
//
//        underTest.resolve(null, template);
//
//        InOrder orderVerifier = inOrder(one, two, three);
//
//        orderVerifier.verify(one).resolve(null, template);
//        orderVerifier.verify(two).resolve(null, template);
//        orderVerifier.verify(three).resolve(null, template);
//    }
}