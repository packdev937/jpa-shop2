package jpabook.jpashop.api;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MemberApiController {
    private final MemberService memberService;

    // 엔티티를 직접 노출하게 되면 List<Member>의 Member
    // 모든 값들이 다 노출되게 된다
    // @JsonIgnore을 사용하자 -> 얘가 붙은 것들은 조회에서 빠진다.
    // 하지만 조회는 다양한 곳에서 사용되는 만큼 케이스 또한 엄청 많다. 엔티티 하나 하나를 조절하면서 하는 것은 답이 없다
    // api를 만들 떄는 엔티티를 노출하지마라! 중간에 스펙에 맞는 DTO를 만들라 강조!!
    @GetMapping("/api/v1/members")
    public List<Member> membersV1() {
        return memberService.findMembers();
    }

    @GetMapping("/api/v2/members")
    public Result memberV2() {
        List<Member> findMembers = memberService.findMembers();
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName()))
                .collect(Collectors.toList());

        return new Result(collect.size(), collect);
    }

    // 이렇게 감싸주는 이유
    // 확장성을 지니기 위해서
    // 그냥 List를 리턴하게 되면 [ ] 배열의 형태로 감싸지게 된다
    // 하지만 만약에 count , [] 배열을 같이 넘기고 싶다면 어떤가
    // 감싸져있기 때문에 못한다
    // { count:1, data: [ ~~] } 이런식으로 되야지 확장성이 높다고 할 수 있는 것
    @Data
    @AllArgsConstructor
    static class Result<T> {
        private int count;
        private T data;
    }

    @Data
    @AllArgsConstructor
    static class MemberDto {
        private String name;
    }

    // JSON으로 온 Body를 Member에 매핑해서 넘겨준다.
    // 만약에 name = "hello"만 채워주면 나머지는 다 Null로 채워진다.
    // 만약에 아무것도 넘기지 않는다면 -> 모두 Null로 채워진다.
    // @Valid Member 있기 때문에 옵션들을 설정하면 이를 방지할 수 있다.
    // @NotEmpty name;
    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    // DTO를 받는 법은 DTO로 요청을 받고
    // 보통 메서드들은 다 Member를 받기 때문에 이를 Member 객체로 변환해야한다
    // 이는 Setter, Getter를 통해 해결한다
    // 2번의 장점 -> API 스펙은 잘 바귀지 않는다
    // 만약에 값을 변경한 시점에 컴파일 에러가 나기 떄문에 바꿔주면 된다 (중간에서 변경 사실을 눈치챌 수 있음)

    // DTO로 받으면 좋은 점 -> DTO를 확인함으로써 아~ 이 DTO는 이 변수를 API 스펙으로 받구나를 확인할 수 있음
    // 만약에 Member 엔티티를 넘겨준다면 해당 memberEntity에 어떤 값이 들어오는지 알 수 없다.
    // 또한 각 DTO 개체 마다 Valid를 다르게 설정할 수도 있다.
    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        Member newMember = new Member();
        newMember.setName(request.getName());

        Long id = memberService.join(newMember);
        return new CreateMemberResponse(id);
    }

    // 회원 업데이트
    // PUT은 같은걸 여러번 해도 변경되지 않음
    // 별도의 DTO를 가져간다

    // 여기서 return UpdateMemberResponse를 반환한다고 가정해보자
    // 변경할 내용은 memberService의 메소드 내에서 변경 감지로 바뀌었다
    // 이 때 어떻게 해서 해당 변경 객체를 불러올 수 있을까?
    @PutMapping("/api/v3/members/{id}")
    public UpdateMemberResponse updateMemberV2(
            @PathVariable("id") Long id,
            @RequestBody @Valid UpdateMemberRequest request) {
        memberService.update(id, request.getName()); // 여기서 끝내자
        Member findMember = memberService.findOne(id); // 아니면 직접 불러오자
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    // Entity는 롬복을 Getter로 제한하지만
    // DTO는 막쓴다 (개인 취향)
    @Data
    static class UpdateMemberRequest {
        private String name;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }

    @Data
    static class CreateMemberRequest {
        private String name;
    }

    // 왜 객체를 만들어서 return 하는가?
    // JSON 형태로 id 를 반환하기 위해?
    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }
}

// 심각함 문제
// Presentation에 대한 검증 로직이 모두 Entity에 들어간다.
// 어떤 Entity는 @NotEmpty가 필요한데, 다른 Entity는 @NotEmpty가 필요하지 않을 수 있다.
// 즉, Member member를 가져다 쓸 때 각각이 요구하는 Validation Rule이 다르다
// Member Entity에 대한 값을 수정할 경우 API 스펙이 바뀌어 버린다
// name -> username으로 바꿔버리면 {"username" : "pack"} 으로 접근해야됨
// Entity는 여러 곳에서 쓰기 때문에 바뀔 확률이 높다 하지만 Entity Spec 자체가 변하는 것은 안된다
// 따라서 API 스펙을 위한 별도의 DTO를 만들어야 한다
// 아! 왜 DTO가 필요한지 알겠다 -> Entity를 매개변수로서 쓰면 안된다!
// Entity를 외부에 요청하지 마라!
// 파라미터로 Entity로 받지말고 외부에 노출하지도 마라!

//->
// Spring binding에 대해 공부
// DTO를 왜 쓰는가 작성