import sys
import re
import time
import random
import logging
import requests
import pyperclip
from bs4 import BeautifulSoup

# --- 로깅 설정 ---
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# --- 상수 ---
BASE_URL = "https://btdig.com/search"
USER_AGENTS = [
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.1 Safari/605.1.15',
    'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36',
]

# --- Tor 프록시 설정 ---
PROXIES = {
    'http': 'socks5h://127.0.0.1:9150',
    'https': 'socks5h://127.0.0.1:9150'
}

# --- 데이터 클래스 ---
class MagnetInfo:
    """토렌트 정보를 저장하는 데이터 클래스"""
    def __init__(self):
        self.magnet_link = None
        self.filenames = []

    def __repr__(self):
        return f"MagnetInfo(magnet_link='{self.magnet_link}', files={len(self.filenames)})"

# --- 핵심 로직 ---
def search_torrent(search_params):
    """
    btdig.com에서 주어진 검색어로 토렌트를 검색하고 파싱합니다.
    """
    if not search_params:
        return []

    query = "+".join(search_params)
    search_url = f"{BASE_URL}?q={query}&order=2"
    logging.info(f"* URL 호출: {search_url}")

    headers = {
        'User-Agent': random.choice(USER_AGENTS)
    }
    
    MAX_RETRIES = 2
    RETRY_DELAY = 60 # seconds

    for attempt in range(MAX_RETRIES):
        try:
            # Tor 프록시를 사용하여 요청
            logging.info("Tor 프록시를 통해 요청을 보냅니다...")
            response = requests.get(search_url, headers=headers, proxies=PROXIES, timeout=30)
            
            if response.status_code == 429:
                logging.warning(f"429 오류 (Too Many Requests). {RETRY_DELAY}초 후 재시도합니다... (시도 {attempt + 1}/{MAX_RETRIES})")
                time.sleep(RETRY_DELAY)
                continue

            response.raise_for_status()
            # 요청이 성공하면 루프를 빠져나갑니다.
            soup = BeautifulSoup(response.text, 'html.parser')
            results = soup.find_all('div', class_='one_result')
            magnet_info_list = []

            for result in results:
                torrent_excerpt = result.find('div', class_='torrent_excerpt')
                torrent_magnet = result.find('div', class_='torrent_magnet')

                if not torrent_excerpt or not torrent_magnet:
                    continue

                info = MagnetInfo()

                # 파일 이름 추출
                file_elements = torrent_excerpt.find_all('span', class_='fa-file')
                for file_element in file_elements:
                    # 'fa-file' 클래스를 가진 span의 부모 요소(div)에서 텍스트를 가져옵니다.
                    if file_element.parent and file_element.parent.text:
                        info.filenames.append(file_element.parent.text.strip())


                # 마그넷 링크 추출
                magnet_link_element = torrent_magnet.find('a', class_='fa-magnet')
                if magnet_link_element and 'href' in magnet_link_element.attrs:
                    info.magnet_link = magnet_link_element['href']

                if info.magnet_link and info.filenames:
                    magnet_info_list.append(info)

            return magnet_info_list

        except requests.RequestException as e:
            logging.error(f"웹사이트에 연결하는 중 오류 발생: {e}")
            # 재시도 로직에서 이 예외는 즉시 반환하는 것이 나을 수 있습니다.
            break 
    
    # 루프가 break 없이 완료되면 (모든 재시도 실패)
    logging.error(f"최대 재시도 횟수({MAX_RETRIES})에 도달했습니다. '{' '.join(search_params)}' 검색을 건너뜁니다.")
    return []

def main():
    """
    메인 실행 함수.
    """
    args = sys.argv[1:]
    if not args:
        print("마그넷을 찾기 위한 검색어를 입력해야 합니다.")
        print("  사용법 (단일 파일): > python torrent_seeker.py ubuntu desktop iso")
        print("  사용법 (다중 파일): > python torrent_seeker.py game of thrones s1 e[1-10]")
        return

    all_magnet_links = []
    search_tasks = []
    
    # 인수 파싱
    base_args = []
    range_prefix = ""
    start_num, end_num = 1, 1
    
    range_pattern = re.compile(r"(.+?)\[(\d+)-(\d+)\]")
    
    found_range = False
    for arg in args:
        match = range_pattern.match(arg)
        if match:
            range_prefix = match.group(1)
            start_num = int(match.group(2))
            end_num = int(match.group(3))
            found_range = True
        else:
            base_args.append(arg)
            
    if not found_range:
        search_tasks.append(base_args)
    else:
        num_length = len(str(end_num))
        for i in range(start_num, end_num + 1):
            current_params = list(base_args)
            num_str = str(i).zfill(num_length)
            current_params.append(f"{range_prefix}{num_str}")
            search_tasks.append(current_params)
            
    # 검색 실행
    for params in search_tasks:
        logging.info(f"실제 검색 파라미터 -> {' '.join(params)}")
        
        search_results = search_torrent(params)
        
        if not search_results:
            logging.warning(f"'{' '.join(params)}'에 대한 검색 결과가 없습니다.")
            continue

        # 키워드 필터링
        contain_list = []
        for magnet_info in search_results:
            for filename in magnet_info.filenames:
                is_contained_all = all(
                    keyword.lower() in filename.lower() for keyword in params
                )
                if is_contained_all:
                    contain_list.append(magnet_info)
                    break # 다음 마그넷 정보로 넘어감
        
        # 최적 마그넷 선택
        if not contain_list:
            logging.warning(f"파일 이름에 모든 키워드 '{' '.join(params)}'를 포함하는 마그넷을 찾지 못했습니다.")
            continue
            
        chosen_magnet = min(contain_list, key=lambda x: len(x.filenames))

        if chosen_magnet and chosen_magnet.magnet_link:
            logging.info(f"* 키워드를 포함하는 마그넷이 추가되었습니다. (파일 개수: {len(chosen_magnet.filenames)})")
            all_magnet_links.append(chosen_magnet.magnet_link)
        
        # 딜레이
        sleep_time = 10 + random.random() * 10
        logging.info(f"{sleep_time:.2f}초 대기...")
        time.sleep(sleep_time)

    # 클립보드에 복사
    if all_magnet_links:
        final_string = "\n".join(all_magnet_links)
        pyperclip.copy(final_string)
        logging.info(f"총 {len(all_magnet_links)}개의 마그넷 링크를 클립보드에 복사했습니다.")
    else:
        logging.info("클립보드에 복사할 마그넷 링크가 없습니다.")


if __name__ == "__main__":
    main() 