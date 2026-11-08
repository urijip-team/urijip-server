package com.urijip.server.global.push;

import java.util.Collection;
import java.util.Set;

// 기기 토큰들에 푸시를 보낸다. 앱이 지워졌거나 만료돼 다시 쓸 수 없는 토큰을 돌려주면, 호출한 쪽이 저장소에서 지운다.
public interface PushSender {

	Set<String> send(Collection<String> tokens, PushMessage message);

}
