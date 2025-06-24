/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.system;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import java.util.concurrent.TimeUnit;

@Controller
class WelcomeController {

    private static final String WELCOME_VIEW = "welcome";
    private static final long CACHE_DURATION = 3600; // 1 hour in seconds

    @GetMapping("/")
    @Cacheable(value = "welcomePageCache", key = "'welcomePage'")
    public ResponseEntity<String> welcome() {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(CACHE_DURATION, TimeUnit.SECONDS))
            .body(WELCOME_VIEW);
    }

}