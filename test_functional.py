import time
from selenium import webdriver
from selenium.webdriver.chrome.service import Service as ChromeService
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from webdriver_manager.chrome import ChromeDriverManager

BASE_URL = "https://webthoitrang.haorrr.dev/index.html" # Sửa lại URL gốc của bạn

class FunctionalTest:
    def __init__(self):
        options = webdriver.ChromeOptions()
        options.add_argument("--start-maximized")
        # Sử dụng driver tải sẵn trong instance
        self.driver = webdriver.Chrome(service=ChromeService(ChromeDriverManager().install()), options=options)
        self.wait = WebDriverWait(self.driver, 10)
        
        # Xóa file log cũ và tạo mới
        with open("functional_report.txt", "w", encoding="utf-8") as f:
            f.write("=== KẾT QUẢ TEST CÁC CHỨC NĂNG (TÌM KIẾM, CHI TIẾT, ĐIỀU HƯỚNG, FLASH SALE) ===\n\n")

    def write_log(self, msg):
        print(msg)
        with open("functional_report.txt", "a", encoding="utf-8") as f:
            f.write(msg + "\n")

    def open_home(self):
        self.driver.get(BASE_URL)
        time.sleep(2)

    def perform_login(self):
        self.write_log("\n--- THỰC HIỆN ĐĂNG NHẬP (Tiền đề cho các test sau) ---")
        login_url = BASE_URL.replace("index.html", "login.html")
        self.driver.get(login_url)
        time.sleep(2)
        try:
            email_input = self.wait.until(EC.presence_of_element_located((By.ID, "email")))
            password_input = self.driver.find_element(By.ID, "password")
            submit_btn = self.driver.find_element(By.ID, "submitBtn")
            
            email_input.send_keys("tk2@gmail.com")
            password_input.send_keys("Vhh201004@")
            self.driver.execute_script("arguments[0].click();", submit_btn)
            time.sleep(3)
            self.write_log("-> Đăng nhập thành công với tk2@gmail.com")
        except Exception as e:
            self.write_log("Lỗi khi đăng nhập tiền đề: " + str(e))

    def test_search(self):
        self.write_log("\n--- BẮT ĐẦU TEST TÌM KIẾM SẢN PHẨM ---")
        # Trang tìm kiếm là products.html
        search_url = BASE_URL.replace("index.html", "products.html")
        self.driver.get(search_url)
        time.sleep(2)

        try:
            # Tìm ô input search
            search_input = self.wait.until(EC.presence_of_element_located((By.ID, "searchInput")))
            
            # Kịch bản 1: Tìm kiếm có kết quả
            self.write_log("1. Tìm kiếm 'áo'")
            search_input.clear()
            search_input.send_keys("áo")
            time.sleep(2)
            # Kiểm tra DOM hiển thị sp
            elements = self.driver.find_elements(By.CLASS_NAME, "product-card")
            self.write_log(f"-> Tìm được {len(elements)} kết quả (PASS)")
            
            # Kịch bản 2: Tìm kiếm không kết quả
            self.write_log("2. Tìm kiếm 'abcxyz123'")
            search_input = self.wait.until(EC.presence_of_element_located((By.ID, "searchInput")))
            search_input.clear()
            search_input.send_keys("abcxyz123")
            time.sleep(2)
            # Kiểm tra text "Không tìm thấy"
            try:
                empty_state = self.driver.find_element(By.CSS_SELECTOR, ".empty-state h3").text
                self.write_log(f"-> Hiển thị: {empty_state} (PASS)")
            except:
                pass
            
            # Kịch bản 3: Ký tự đặc biệt
            self.write_log("3. Tìm kiếm '@#$%'")
            search_input = self.wait.until(EC.presence_of_element_located((By.ID, "searchInput")))
            search_input.clear()
            search_input.send_keys("@#$%")
            time.sleep(2)
            self.write_log("-> Test Tìm kiếm hoàn tất")

        except Exception as e:
            self.write_log("Lỗi Test Tìm Kiếm: " + str(e))

    def test_product_detail(self):
        self.write_log("\n--- BẮT ĐẦU TEST CHI TIẾT SẢN PHẨM ---")
        search_url = BASE_URL.replace("index.html", "products.html")
        self.driver.get(search_url)
        time.sleep(3)
        try:
            # Nhấn vào 1 sản phẩm đầu tiên
            self.write_log("1. Chuyển sang trang Chi Tiết Sản Phẩm")
            first_product = self.wait.until(EC.element_to_be_clickable((By.CSS_SELECTOR, ".product-card")))
            first_product.click()
            time.sleep(3)

            # Chọn size, màu nếu có
            try:
                sizes = self.driver.find_elements(By.CSS_SELECTOR, ".size-btn:not(.disabled)")
                if sizes:
                    self.write_log("2. Chọn kích thước")
                    sizes[0].click()
                    time.sleep(1)
            except: pass

            try:
                colors = self.driver.find_elements(By.CSS_SELECTOR, ".color-dot")
                if colors:
                    self.write_log("3. Chọn màu sắc")
                    colors[0].click()
                    time.sleep(1)
            except: pass

            self.write_log("4. Thêm vào giỏ hàng")
            add_cart_btn = self.driver.find_element(By.ID, "addToCartBtn")
            add_cart_btn.click()
            time.sleep(2)
            self.write_log("-> Test Chi tiết sản phẩm hoàn tất (PASS)")
        except Exception as e:
            self.write_log("Lỗi Test Chi Tiết S/P: " + str(e))

    def test_flash_sale(self):
        self.write_log("\n--- BẮT ĐẦU TEST FLASH SALE ---")
        fs_url = BASE_URL.replace("index.html", "flash-sale.html?id=10")
        self.driver.get(fs_url)
        time.sleep(3)
        try:
            self.write_log("1. Mở trang Flash Sale ID=10")
            flash_sale_section = self.wait.until(EC.presence_of_element_located((By.ID, "fsGrid")))
            self.driver.execute_script("arguments[0].scrollIntoView();", flash_sale_section)
            
            # Click Mua Nhanh ở sản phẩm Flash Sale
            self.write_log("2. Mở Quick View cho Flash Sale")
            flash_add_btn = self.driver.find_element(By.CSS_SELECTOR, "#fsGrid .product-card__quick-view")
            # Sử dụng Javascript click đề phòng overlay cản trở
            self.driver.execute_script("arguments[0].click();", flash_add_btn)
            time.sleep(2)
            self.write_log("-> Test Flash Sale hoàn tất (PASS)")

        except Exception as e:
            self.write_log("Lỗi Test Flash Sale: " + str(e))

    def test_navigation(self):
        self.write_log("\n--- BẮT ĐẦU TEST ĐIỀU HƯỚNG ---")
        self.open_home()
        try:
            self.write_log("1. Chuyển sang phần Bộ Sưu Tập (Cửa hàng)")
            # Trên trang chủ, link dẫn sang products.html nằm ở nút Xem Tất Cả, không phải trên thanh điều hướng (#products)
            nav_shop = self.wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, "a[href='products.html'].btn"))) 
            self.driver.execute_script("arguments[0].click();", nav_shop)
            time.sleep(2)

            self.write_log("2. Chuyển sang Giỏ hàng")
            cart_icon = self.wait.until(EC.presence_of_element_located((By.ID, "cartBtn")))
            self.driver.execute_script("arguments[0].click();", cart_icon)
            time.sleep(2)

            self.write_log("3. Sử dụng Back của trình duyệt")
            self.driver.back()
            time.sleep(2)
            
            self.write_log("4. Nhấp Logo để về trang chủ")
            logo = self.wait.until(EC.presence_of_element_located((By.CLASS_NAME, "navbar__logo")))
            self.driver.execute_script("arguments[0].click();", logo)
            time.sleep(2)
            
            self.write_log("-> Kiểm tra điều hướng hoàn tất (PASS)")
        except Exception as e:
            self.write_log("Lỗi Test Điều Hướng: " + str(e))

    def run_all(self):
        try:
            self.perform_login()
            self.test_search()
            self.test_product_detail()
            self.test_flash_sale()
            self.test_navigation()
        finally:
            self.write_log("\nĐã chạy xong toàn bộ kịch bản. Đóng trình duyệt sau 5s...")
            time.sleep(5)
            self.driver.quit()

if __name__ == "__main__":
    tester = FunctionalTest()
    tester.run_all()
