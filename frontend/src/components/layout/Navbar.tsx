import { Menu, Globe, Bell } from 'lucide-react';

interface NavbarProps {
    onMenuClick: () => void;
}

export const Navbar = ({ onMenuClick }: NavbarProps) => {
    return (
        <header className="bg-white border-b border-gray-200 h-16 fixed top-0 left-0 right-0 z-30">
            <div className="h-full px-4 flex items-center justify-between">

                <div className="flex items-center gap-4">
                    <button onClick={onMenuClick} className="p-2 rounded-lg hover:bg-gray-100 lg:hidden">
                        <Menu className="w-6 h-6 text-gray-600" />
                    </button>
                    <div className="flex items-center gap-2">
                        <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center">
                            <span className="text-white font-bold text-lg">A</span>
                        </div>
                        <span className="font-semibold text-gray-900 hidden sm:block">Adaptivno Učenje</span>
                    </div>
                </div>

                <div className="flex items-center gap-2">
                    <button className="p-2 rounded-lg hover:bg-gray-100 flex items-center gap-1">
                        <Globe className="w-5 h-5 text-gray-600" />
                        <span className="text-sm font-medium text-gray-600">HR</span>
                    </button>
                    <button className="p-2 rounded-lg hover:bg-gray-100 relative">
                        <Bell className="w-5 h-5 text-gray-600" />
                    </button>
                    <button className="p-2 rounded-lg hover:bg-gray-100 flex items-center gap-2">
                        <div className="w-8 h-8 bg-indigo-100 rounded-full flex items-center justify-center">
                            <span className="text-indigo-600 font-medium text-sm">MČ</span>
                        </div>
                    </button>
                </div>

            </div>
        </header>
    );
};