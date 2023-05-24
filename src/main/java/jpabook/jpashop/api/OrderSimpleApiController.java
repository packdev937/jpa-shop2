package jpabook.jpashop.api;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Order를 조회하고
// Order와 Member가 연관이 걸리고
// Order와 Delivery가 연관이 걸리도록
// Order와 Member의 관계
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        return all;
    }

    // 무한루프에 빠진다
    // 왜? order -> Member -> order -> Member -> order
    // 양방향 연관관계가 있으면 둘 중 하나는 JsonIgnore하여야 한다
    // 첫 번째 문제는 해결

    // 두 번째 문제
    // Order을 가지고 옴 -> Order는 fetch가 LAZY로 되어있음
    // 지연로딩이란? 포스팅 하자
    // 지연 로딩은 DB에서 Order만 가져온다 -> Member는 안가져옴
    // 따라서 Proxy Member 객체를 생성해서 넣어둠 (ByteBuddyInterceptor())
    // Hibernate 모듈을 설치해야 함
    // hibernate5란? 포스팅 하자
}
